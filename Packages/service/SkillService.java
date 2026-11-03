package com.college.placement.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.placement.entity.Skill;
import com.college.placement.repository.SkillRepository;

@Service
public class SkillService {

    private final SkillRepository skills;

    public SkillService(SkillRepository skills) {
        this.skills = skills;
    }

    @Transactional
    public Set<Skill> resolve(Set<String> names) {
        Set<Skill> result = new HashSet<>();
        if (names == null) {
            return result;
        }
        for (String raw : names) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String name = raw.trim();
            Skill skill = skills.findByNameIgnoreCase(name).orElseGet(() -> skills.save(new Skill(name)));
            result.add(skill);
        }
        return result;
    }
}
