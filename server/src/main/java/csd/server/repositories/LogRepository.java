package csd.server.repositories;

import csd.server.models.LogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<LogEntity, Long> {

	@Query("select logEntity from LogEntity logEntity where logEntity.originContract = :contract or logEntity.destinationContract = :contract")
	List<LogEntity> getLogEntitiesByContract(@Param("contract") String contract);

}
