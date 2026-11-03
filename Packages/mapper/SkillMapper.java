package com.college.placement.mapper;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.college.placement.entity.Skill;

@Component
public class SkillMapper {

    public Set<String> toNames(Set<Skill> skills) {
        return skills.stream().map(Skill::getName).collect(Collectors.toCollection(TreeSet::new));
    }
}
