package cn.iocoder.yudao.module.rental.integration.logistics;

/**
 * Provider-neutral boundary. Supplier SDK types must not cross this interface.
 */
public interface LogisticsProvider {

    String providerCode();

    LogisticsOperationResult subscribe(LogisticsSubscribeCommand command);

    LogisticsOperationResult query(LogisticsQueryCommand command);

    LogisticsOperationResult parseVerifiedCallback(LogisticsCallbackCommand command);
}
