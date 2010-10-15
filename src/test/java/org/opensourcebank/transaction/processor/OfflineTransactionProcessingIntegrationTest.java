/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensourcebank.transaction.processor;

import com.hazelcast.core.Hazelcast;
import org.gridgain.grid.GridFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensourcebank.transaction.iso8583.ISO8583Transaction;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * <p>Using {@link org.springframework.batch.item.file.FlatFileItemReader} stages transactions from a file to Hazelcast
 *    and runs the job over multiple GridGain nodes</p>
 *
 * @author anatoly.polinsky
 */
@ContextConfiguration(locations = {"classpath:/META-INF/conf/offline-tx-processing-test-context.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class OfflineTransactionProcessingIntegrationTest {

	static {
    	System.setProperty( "GRIDGAIN_HOME", "/opt/gridgain" );
	}

	@Autowired
	private JobLauncher jobLauncher;
	@Autowired
	private Job job;
	@Autowired
	@Qualifier("step")
	private Step step;

    @Autowired
    private FlatFileItemReader<ISO8583Transaction> stagingItemReader;

    @Before
    public void stageTransactionsFromFileToCache() throws Exception {

        Map<Long, ISO8583Transaction> txMap =  Hazelcast.getMap( "offline-transactions" );

        //IdGenerator idGenerator = Hazelcast.getIdGenerator("txIds");
        //long id = idGenerator.newId();

        ISO8583Transaction tx;
        Long txId = 0L;

        stagingItemReader.open( new ExecutionContext() );
        while ( ( tx = stagingItemReader.read() ) != null) {
            // in reality here you would check if the key already exists, so the entry is not overwritten
            txMap.put( ++txId, tx );
        }
    }

	@Test
	public void shouldAutowireJobLauncherAndStep() throws Exception {
		assertNotNull(jobLauncher);
		assertNotNull(step);
	}

	@Test
	public void shouldLaunchJob() throws Exception {
    		assertNotNull(jobLauncher.run(job,
                new JobParametersBuilder().addString(
                        "run.id",
                        "offline-transaction-processing-integration.test").toJobParameters()));
	}

    @BeforeClass
    public static void startGridFactory() throws Exception {
        GridFactory.start();
    }

    @AfterClass
    public static void stopGridFactory() throws Exception {
        GridFactory.stop( true );
    }
}