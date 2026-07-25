package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class XianyuPushEventPublisher {

    private final ObjectProvider<XianyuOrderPushConsumer> orderPushConsumer;
    private final ObjectProvider<XianyuProductPushConsumer> productPushConsumer;

    public XianyuPushEventPublisher(ObjectProvider<XianyuOrderPushConsumer> orderPushConsumer,
                                    ObjectProvider<XianyuProductPushConsumer> productPushConsumer) {
        this.orderPushConsumer = orderPushConsumer;
        this.productPushConsumer = productPushConsumer;
    }

    public void publishAfterCommitOrNow(Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

                @Override
                public void afterCommit() {
                    dispatch(event);
                }

            });
            return;
        }
        dispatch(event);
    }

    private void dispatch(Object event) {
        if (event instanceof XianyuOrderPushReceivedEvent orderEvent) {
            orderPushConsumer.getObject().onOrderPush(orderEvent);
            return;
        }
        if (event instanceof XianyuProductPushReceivedEvent productEvent) {
            productPushConsumer.getObject().onProductPush(productEvent);
            return;
        }
        throw new IllegalArgumentException("Unsupported XianGuanJia push event type");
    }

}
