package com.a6raywa1cher.rescheduletsuvk.services.impls;

import com.a6raywa1cher.rescheduletsuvk.dao.StudentUserRepository;
import com.a6raywa1cher.rescheduletsuvk.dao.TeacherUserRepository;
import com.a6raywa1cher.rescheduletsuvk.models.StudentUser;
import com.a6raywa1cher.rescheduletsuvk.models.TeacherUser;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Transactional
public class UserInfoServiceImpl implements UserInfoService {
	private TeacherUserRepository teacherUserRepository;
	private StudentUserRepository studentUserRepository;


	@Autowired
	public UserInfoServiceImpl(TeacherUserRepository teacherUserRepository, StudentUserRepository studentUserRepository) {
		this.teacherUserRepository = teacherUserRepository;
		this.studentUserRepository = studentUserRepository;
	}

	@Override
	public Optional<? extends UserInfo> getById(Integer id) {
		Optional<StudentUser> studentUser = studentUserRepository.findById(id);
		if (studentUser.isPresent()) {
			return studentUser;
		}
		return teacherUserRepository.findById(id);
	}

	@Override
	public <T extends UserInfo> T save(T userInfo) {
		if (userInfo instanceof StudentUser) {
			return (T) studentUserRepository.save((StudentUser) userInfo);
		} else if (userInfo instanceof TeacherUser) {
			return (T) teacherUserRepository.save((TeacherUser) userInfo);
		} else if (userInfo == null) {
			return null;
		} else {
			throw new IllegalArgumentException("Unknown userInfo impl: " + userInfo.getClass().getName());
		}
	}

	@Override
	public <T extends UserInfo> void delete(T userInfo) {
		if (userInfo instanceof StudentUser) {
			studentUserRepository.delete((StudentUser) userInfo);
		} else if (userInfo instanceof TeacherUser) {
			teacherUserRepository.delete((TeacherUser) userInfo);
		} else if (userInfo != null) {
			throw new IllegalArgumentException("Unknown userInfo impl: " + userInfo.getClass().getName());
		}
	}
}
