package org.omt.labelmanager.sales.distributorreturn.application;

import java.util.List;
import java.util.Optional;
import org.omt.labelmanager.sales.distributorreturn.api.DistributorReturnQueryApi;
import org.omt.labelmanager.sales.distributorreturn.domain.DistributorReturn;
import org.omt.labelmanager.sales.distributorreturn.infrastructure.DistributorReturnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DistributorReturnQueryApiImpl implements DistributorReturnQueryApi {

    private final DistributorReturnRepository returnRepository;
    private final ReturnConverter returnConverter;

    DistributorReturnQueryApiImpl(
            DistributorReturnRepository returnRepository, ReturnConverter returnConverter) {
        this.returnRepository = returnRepository;
        this.returnConverter = returnConverter;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistributorReturn> getReturnsForLabel(Long labelId) {
        return returnRepository.findByLabelIdOrderByReturnDateDesc(labelId).stream()
                .map(returnConverter::toReturn)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistributorReturn> getReturnsForDistributor(Long distributorId) {
        return returnRepository.findByDistributorIdOrderByReturnDateDesc(distributorId).stream()
                .map(returnConverter::toReturn)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DistributorReturn> findById(Long returnId) {
        return returnRepository.findById(returnId).map(returnConverter::toReturn);
    }
}
