package com.a6raywa1cher.rescheduletsuvk.dao.interfaces;

import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;

import java.util.Optional;

public interface UserInfoService {
	Optional<UserInfo> getById(Integer id);

	UserInfo save(UserInfo userInfo);
}
