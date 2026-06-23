package com.unimobili.agenda.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    @Query("""
            select (count(e) > 0) from Event e
            where e.externalUser.id = :externalUserId
              and e.status in :statuses
              and e.dataHoraInicio < :fim
              and :inicio < e.dataHoraFim
            """)
    boolean existsConflict(@Param("externalUserId") UUID externalUserId,
                           @Param("statuses") Collection<EventStatus> statuses,
                           @Param("inicio") Instant inicio,
                           @Param("fim") Instant fim);

    @Query("""
            select (count(e) > 0) from Event e
            where e.externalUser.id = :externalUserId
              and e.id <> :excludeId
              and e.status in :statuses
              and e.dataHoraInicio < :fim
              and :inicio < e.dataHoraFim
            """)
    boolean existsConflictExcluding(@Param("excludeId") UUID excludeId,
                                    @Param("externalUserId") UUID externalUserId,
                                    @Param("statuses") Collection<EventStatus> statuses,
                                    @Param("inicio") Instant inicio,
                                    @Param("fim") Instant fim);
}
