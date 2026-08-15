package com.futbol.majo.repository.specification;

import com.futbol.majo.entity.MatchEntity;
import com.futbol.majo.entity.TeamEntity;
import com.futbol.majo.repository.MatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MatchSpecificationTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private MatchRepository matchRepository;

  private TeamEntity realMadrid;
  private TeamEntity barcelona;
  private TeamEntity atletico;

  @BeforeEach
  void setUp() {
    realMadrid = new TeamEntity(1L, "Real Madrid CF", "Real Madrid", "crest_rm.png");
    barcelona = new TeamEntity(2L, "FC Barcelona", "Barça", "crest_bar.png");
    atletico = new TeamEntity(3L, "Atlético de Madrid", "Atleti", "crest_atm.png");

    entityManager.persist(realMadrid);
    entityManager.persist(barcelona);
    entityManager.persist(atletico);

    MatchEntity match1 = new MatchEntity();
    match1.setId(101L);
    match1.setCompetitionCode("PD");
    match1.setMatchDay(1);
    match1.setStatus("FINISHED");
    match1.setUtcDate(OffsetDateTime.parse("2026-08-15T18:00:00Z"));
    match1.setHomeTeam(realMadrid);
    match1.setAwayTeam(barcelona);

    MatchEntity match2 = new MatchEntity();
    match2.setId(102L);
    match2.setCompetitionCode("PD");
    match2.setMatchDay(2);
    match2.setStatus("SCHEDULED");
    match2.setUtcDate(OffsetDateTime.parse("2026-08-22T20:00:00Z"));
    match2.setHomeTeam(atletico);
    match2.setAwayTeam(realMadrid);

    entityManager.persist(match1);
    entityManager.persist(match2);
    entityManager.flush();
  }

  @Test
  @DisplayName("Debe filtrar partidos por jornada (matchDay)")
  void testHasMatchday() {
    Specification<MatchEntity> spec = MatchSpecification.hasMatchday(1);
    List<MatchEntity> results = matchRepository.findAll(spec);

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().getMatchDay()).isEqualTo(1);
  }

  @Test
  @DisplayName("Debe filtrar partidos por estado (status)")
  void testHasStatus() {
    Specification<MatchEntity> spec = MatchSpecification.hasStatus("SCHEDULED");
    List<MatchEntity> results = matchRepository.findAll(spec);

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().getStatus()).isEqualTo("SCHEDULED");
  }

  @Test
  @DisplayName("Debe encontrar partidos donde un equipo sea local O visitante por su teamId")
  void testHasTeamId() {
    // Atlético juega como local en match1 y visitante en match2 -> Debe devolver 1 partido
    Specification<MatchEntity> specAtleti = MatchSpecification.hasTeamId(atletico.getId());
    List<MatchEntity> resultsAtleti = matchRepository.findAll(specAtleti);

    assertThat(resultsAtleti).hasSize(1);

    // Real Madrid juega como local en match1 y visitante en match2 -> Debe devolver 2 partidos
    Specification<MatchEntity> specRM = MatchSpecification.hasTeamId(realMadrid.getId());
    List<MatchEntity> resultsRM = matchRepository.findAll(specRM);

    assertThat(resultsRM).hasSize(2);

    // Barcelona solo juega en match1 como visitante -> Debe devolver 1 partido
    Specification<MatchEntity> specBarca = MatchSpecification.hasTeamId(barcelona.getId());
    List<MatchEntity> resultsBarca = matchRepository.findAll(specBarca);

    assertThat(resultsBarca).hasSize(1);
  }

  @Test
  @DisplayName("Debe filtrar partidos entre un rango de fechas")
  void testBetweenDates() {
    OffsetDateTime from = OffsetDateTime.parse("2026-08-01T00:00:00Z");
    OffsetDateTime to = OffsetDateTime.parse("2026-08-18T23:59:59Z");

    Specification<MatchEntity> spec = MatchSpecification.betweenDates(from, to);
    List<MatchEntity> results = matchRepository.findAll(spec);

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().getHomeTeam().getName()).isEqualTo("Real Madrid CF");
  }
}