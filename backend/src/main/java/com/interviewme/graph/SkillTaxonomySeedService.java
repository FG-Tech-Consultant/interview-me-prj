package com.interviewme.graph;

import com.interviewme.dto.skill.CreateSkillDto;
import com.interviewme.model.Skill;
import com.interviewme.repository.SkillRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Seeds an initial skill taxonomy into PostgreSQL + Neo4j on first startup.
 * Based on ESCO (European Skills, Competences, Qualifications and Occupations) categories.
 * Skips seeding if skills already exist in the graph.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class SkillTaxonomySeedService {

    private final SkillRepository skillRepository;
    private final GraphSkillSyncService graphSkillSyncService;

    // category → list of [name, description]
    private static final Map<String, List<String[]>> TAXONOMY = Map.of(
            "Programming Languages", List.of(
                    new String[]{"Java", "General-purpose, class-based, object-oriented programming language"},
                    new String[]{"Python", "High-level, interpreted, general-purpose programming language"},
                    new String[]{"TypeScript", "Strongly typed superset of JavaScript"},
                    new String[]{"JavaScript", "High-level scripting language for web development"},
                    new String[]{"Go", "Statically typed, compiled programming language by Google"},
                    new String[]{"Kotlin", "Cross-platform, statically typed programming language"},
                    new String[]{"Rust", "Systems programming language focused on safety and performance"}
            ),
            "Frameworks & Libraries", List.of(
                    new String[]{"Spring Boot", "Java framework for building production-ready microservices"},
                    new String[]{"React", "JavaScript library for building user interfaces"},
                    new String[]{"Node.js", "JavaScript runtime for server-side development"},
                    new String[]{"FastAPI", "Modern, fast Python web framework"},
                    new String[]{"Angular", "TypeScript-based front-end framework by Google"},
                    new String[]{"Vue.js", "Progressive JavaScript framework for building UIs"}
            ),
            "Cloud & DevOps", List.of(
                    new String[]{"Docker", "Platform for building, shipping, and running containerized applications"},
                    new String[]{"Kubernetes", "Container orchestration platform"},
                    new String[]{"AWS", "Amazon Web Services cloud computing platform"},
                    new String[]{"Terraform", "Infrastructure as Code tool by HashiCorp"},
                    new String[]{"CI/CD", "Continuous Integration and Continuous Deployment practices"},
                    new String[]{"Linux", "Open-source operating system kernel"},
                    new String[]{"nginx", "High-performance HTTP server and reverse proxy"}
            ),
            "Data & AI", List.of(
                    new String[]{"PostgreSQL", "Advanced open-source relational database system"},
                    new String[]{"Redis", "In-memory data structure store"},
                    new String[]{"Machine Learning", "Field of AI enabling systems to learn from data"},
                    new String[]{"LLM Integration", "Building applications powered by large language models"},
                    new String[]{"SQL", "Structured Query Language for relational databases"},
                    new String[]{"Data Modeling", "Process of creating a data model for stored data"}
            ),
            "Soft Skills", List.of(
                    new String[]{"Communication", "Ability to convey information effectively"},
                    new String[]{"Problem Solving", "Capability to find solutions to difficult issues"},
                    new String[]{"Teamwork", "Ability to work collaboratively in a group"},
                    new String[]{"Leadership", "Capability to guide and motivate a team"},
                    new String[]{"Agile", "Iterative approach to project management and software development"}
            )
    );

    @PostConstruct
    @Transactional
    public void seed() {
        if (graphSkillSyncService.countSkillNodes() > 0) {
            log.info("Skill graph already populated — skipping taxonomy seed.");
            return;
        }

        log.info("Seeding initial skill taxonomy ({} categories)...", TAXONOMY.size());
        int created = 0;

        for (Map.Entry<String, List<String[]>> entry : TAXONOMY.entrySet()) {
            String category = entry.getKey();
            for (String[] item : entry.getValue()) {
                String name = item[0];
                String description = item[1];

                Skill skill = skillRepository.findByNameIgnoreCase(name).orElse(null);
                if (skill == null) {
                    skill = new Skill();
                    skill.setName(name);
                    skill.setCategory(category);
                    skill.setDescription(description);
                    skill.setIsActive(true);
                    skill = skillRepository.save(skill);
                    created++;
                }

                graphSkillSyncService.syncSkill(skill);
            }
        }

        log.info("Taxonomy seed complete: {} skills created and synced to graph.", created);
    }
}
