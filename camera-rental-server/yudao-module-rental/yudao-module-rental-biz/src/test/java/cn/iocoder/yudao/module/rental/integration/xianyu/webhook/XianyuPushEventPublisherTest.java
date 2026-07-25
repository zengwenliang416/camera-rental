package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuPushEventPublisherTest {

    private final XianyuOrderPushConsumer orderPushConsumer = mock(XianyuOrderPushConsumer.class);
    private final XianyuProductPushConsumer productPushConsumer = mock(XianyuProductPushConsumer.class);
    private final XianyuPushEventPublisher publisher = new XianyuPushEventPublisher(
            provider(orderPushConsumer), provider(productPushConsumer));

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void shouldDispatchImmediatelyWhenNoTransactionIsActive() {
        XianyuProductPushReceivedEvent event = new XianyuProductPushReceivedEvent(
                9L, 41L, 77L, "441160510721413");

        publisher.publishAfterCommitOrNow(event);

        verify(productPushConsumer).onProductPush(event);
    }

    @Test
    void shouldDispatchAfterCommitWhenActualTransactionIsActive() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        XianyuOrderPushReceivedEvent event = new XianyuOrderPushReceivedEvent(
                9L, 42L, 77L, "order-1");

        publisher.publishAfterCommitOrNow(event);

        verify(orderPushConsumer, never()).onOrderPush(event);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        verify(orderPushConsumer).onOrderPush(event);
    }

    private static <T> ObjectProvider<T> provider(T object) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(object);
        return provider;
    }

}
