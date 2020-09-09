package com.a6raywa1cher.rescheduletsuvk.dao;

import com.a6raywa1cher.rescheduletsuvk.dao.submodel.FacultyGroupCount;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserInfoRepository extends CrudRepository<UserInfo, Integer> {
	@Query("select new com.a6raywa1cher.rescheduletsuvk.dao.submodel.FacultyGroupCount(u.facultyId, u.groupId, count(u)) " +
		"from UserInfo u group by u.facultyId, u.groupId")
	List<FacultyGroupCount> getCoursesCount();
}
