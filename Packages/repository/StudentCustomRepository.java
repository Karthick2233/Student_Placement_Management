package com.college.placement.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.college.placement.entity.Skill;
import com.college.placement.entity.Student;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Repository
public class StudentCustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Student> searchStudents(String department, Double minCgpa, Integer graduationYear, String skill) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Student> query = cb.createQuery(Student.class);
        Root<Student> root = query.from(Student.class);

        List<Predicate> filters = new ArrayList<>();
        if (StringUtils.hasText(department)) {
            filters.add(cb.equal(cb.lower(root.<String>get("department")), department.trim().toLowerCase()));
        }
        if (minCgpa != null) {
            filters.add(cb.greaterThanOrEqualTo(root.<Double>get("cgpa"), minCgpa));
        }
        if (graduationYear != null) {
            filters.add(cb.equal(root.<Integer>get("graduationYear"), graduationYear));
        }
        if (StringUtils.hasText(skill)) {
            Join<Student, Skill> skills = root.join("skills");
            filters.add(cb.equal(cb.lower(skills.<String>get("name")), skill.trim().toLowerCase()));
        }

        query.select(root).distinct(true).where(filters.toArray(new Predicate[0]))
                .orderBy(cb.desc(root.<Double>get("cgpa")));
        return entityManager.createQuery(query).getResultList();
    }

    public Double findAverageCgpa(String department) {
        String jpql = StringUtils.hasText(department)
                ? "select avg(s.cgpa) from Student s where lower(s.department) = lower(:department)"
                : "select avg(s.cgpa) from Student s";
        var query = entityManager.createQuery(jpql, Double.class);
        if (StringUtils.hasText(department)) {
            query.setParameter("department", department.trim());
        }
        return query.getSingleResult();
    }
}
