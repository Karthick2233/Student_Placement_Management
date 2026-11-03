package com.college.placement.mapper;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.college.placement.dto.StudentDto.StudentPatchRequest;
import com.college.placement.dto.StudentDto.StudentRequest;
import com.college.placement.dto.StudentDto.StudentResponse;
import com.college.placement.entity.AppUser;
import com.college.placement.entity.Skill;
import com.college.placement.entity.Student;

@Component
public class StudentMapper {

    private final SkillMapper skillMapper;

    public StudentMapper(SkillMapper skillMapper) {
        this.skillMapper = skillMapper;
    }

    public Student toEntity(StudentRequest request, AppUser user, Set<Skill> skills) {
        Student student = new Student(user, request.department().trim(), request.graduationYear(), request.cgpa());
        student.setSkills(skills);
        return student;
    }

    public void updateEntity(Student student, StudentRequest request, Set<Skill> skills) {
        student.setDepartment(request.department().trim());
        student.setGraduationYear(request.graduationYear());
        student.setCgpa(request.cgpa());
        student.setSkills(skills);
    }

    public void patchEntity(Student student, StudentPatchRequest request) {
        if (request.department() != null) {
            student.setDepartment(request.department().trim());
        }
        if (request.graduationYear() != null) {
            student.setGraduationYear(request.graduationYear());
        }
        if (request.cgpa() != null) {
            student.setCgpa(request.cgpa());
        }
    }

    public StudentResponse toResponse(Student student) {
        return new StudentResponse(student.getId(), student.getUser().getName(), student.getUser().getEmail(),
                student.getDepartment(), student.getGraduationYear(), student.getCgpa(),
                skillMapper.toNames(student.getSkills()));
    }
}
