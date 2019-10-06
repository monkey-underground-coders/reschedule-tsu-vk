package com.a6raywa1cher.rescheduletsuvk.services.interfaces;

import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;

import java.util.Optional;

public interface UserInfoService {
	Optional<? extends UserInfo> getById(Integer id);

	<T extends UserInfo> T save(T userInfo);

	<T extends UserInfo> void delete(T userInfo);
}
