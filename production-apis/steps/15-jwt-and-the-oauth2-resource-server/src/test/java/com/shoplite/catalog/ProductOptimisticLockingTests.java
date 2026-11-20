package com.shoplite.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.shoplite.TestcontainersConfiguration;

/**
 * Two users load the same product, both change it, both save. With @Version the second
 * save fails instead of silently overwriting the first one (a "lost update").
 * The test turns off the usual rollback-per-test transaction so each save commits for real.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProductOptimisticLockingTests {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Test
    void secondSaveOfAStaleCopyFails() {
        Product firstCopy = load(2L);
        Product secondCopy = load(2L);
        long versionBefore = firstCopy.getVersion();

        firstCopy.setPrice(new BigDecimal("22.00"));
        transactionTemplate.executeWithoutResult(status -> productRepository.save(firstCopy));
        assertThat(load(2L).getVersion()).isEqualTo(versionBefore + 1);

        secondCopy.setStock(100);
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> productRepository.save(secondCopy)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(load(2L).getPrice()).isEqualByComparingTo("22.00");
    }

    private Product load(Long id) {
        return transactionTemplate.execute(status -> productRepository.findById(id).orElseThrow());
    }

}
