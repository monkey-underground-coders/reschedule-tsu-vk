package com.a6raywa1cher.rescheduletsuvk.services.impls;

import com.a6raywa1cher.rescheduletsuvk.dao.UserInfoRepository;
import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import com.a6raywa1cher.rescheduletsuvk.services.interfaces.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserInfoServiceImpl implements UserInfoService {
	private UserInfoRepository repository;

	@Autowired
	public UserInfoServiceImpl(UserInfoRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<UserInfo> getById(Integer id) {
		return repository.findById(id);
	}

	@Override
	public UserInfo save(UserInfo userInfo) {
		return repository.save(userInfo);
	}

	@Override
	public void delete(UserInfo userInfo) {
		repository.delete(userInfo);
	}
}
