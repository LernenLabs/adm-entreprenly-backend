package online.entreprenly.platform.sales.application.queryservices;

import online.entreprenly.platform.sales.domain.model.queries.GetScaleStatusQuery;
import online.entreprenly.platform.sales.domain.model.queries.GetScaleWeightQuery;
import online.entreprenly.platform.sales.domain.model.valueobjects.ScaleStatus;
import online.entreprenly.platform.shared.application.result.ApplicationError;
import online.entreprenly.platform.shared.application.result.Result;

/**
 * Application service contract for IoT scale read operations.
 */
public interface IotScaleQueryService {

    /**
     * Returns the current connection status of the IoT scale.
     *
     * @param query the scale-status query carrying the owner's identity
     * @return the scale status or an application error
     */
    Result<ScaleStatus, ApplicationError> handle(GetScaleStatusQuery query);

    /**
     * Returns the current weight reading (in kg) from the IoT scale.
     *
     * @param query the scale-weight query carrying the owner's identity
     * @return the weight in kg or an application error
     */
    Result<Double, ApplicationError> handle(GetScaleWeightQuery query);
}

