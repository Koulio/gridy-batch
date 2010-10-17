package org.opensourcebank.batch.listener;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.Transaction;
import org.apache.log4j.Logger;
import org.opensourcebank.transaction.iso8583.AbstractIso8583Transaction;
import org.opensourcebank.transaction.iso8583.Iso8583Transaction;
import org.opensourcebank.transaction.iso8583.TransactionStatus;
import org.opensourcebank.transaction.repository.HazelcastIso8583TransactionRepository;
import org.opensourcebank.transaction.repository.Iso8583TransactionRepository;
import org.springframework.batch.core.annotation.*;

import java.util.List;

/**
 * <p>TODO: Add Description</p>
 *
 * @author anatoly.polinsky
 */
public class HazelcastTransactionItemWriteListener {

    private static Logger logger = Logger.getLogger( HazelcastTransactionItemWriteListener.class );
    
    private final ThreadLocal<Transaction> hzTransaction = new ThreadLocal<Transaction>();

    private Iso8583TransactionRepository transactionRepository;


    @BeforeWrite
    public void beginTransaction( List<? extends Iso8583Transaction> transactions ) {

        hzTransaction.set(Hazelcast.getTransaction());
        hzTransaction.get().begin();        

        for ( Iso8583Transaction tx: transactions ) {
            ( (AbstractIso8583Transaction) tx ).setStatus( TransactionStatus.STARTING );
            transactionRepository.update( tx );
        }
    }

    @OnWriteError
    public void rollBackTransaction( Exception exception, List<? extends Iso8583Transaction> items ) {
        hzTransaction.get().rollback();
    }

    @AfterWrite
    public void updateStatusAndCommitTransaction( List<? extends Iso8583Transaction> transactions ) {
        hzTransaction.get().commit();
    }

    
    public void setTransactionRepository(Iso8583TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
}
