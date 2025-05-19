package csd.server.repositories;

import csd.server.models.LedgerEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerRepository extends JpaRepository<LedgerEntity, Long> {

	Optional<LedgerEntity> getLedgerByContract(String contract);

	boolean existsByContract(String contract);

	@Modifying
	@Transactional
	@Query("update LedgerEntity ledger set ledger.value = :value where ledger.contract = :contract")
	int updateValueByContract(@Param("contract") String contract, @Param("value") Long value);

	@Query("select sum(ledger.value) from LedgerEntity ledger where ledger.contract in :contracts")
	long getTotalValue(@Param("contracts") List<String> contracts);

	@Query("select sum(ledger.value) from LedgerEntity ledger")
	long getGlobalLedgerValue();

	@Query("select ledger from LedgerEntity ledger")
	List<LedgerEntity> getAll();
}
