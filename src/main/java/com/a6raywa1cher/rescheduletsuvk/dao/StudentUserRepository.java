package com.a6raywa1cher.rescheduletsuvk.dao;

import com.a6raywa1cher.rescheduletsuvk.models.StudentUser;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentUserRepository extends CrudRepository<StudentUser, Integer> {
}
