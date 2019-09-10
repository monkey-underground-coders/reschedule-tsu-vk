package com.a6raywa1cher.rescheduletsuvk.dao.repository;

import com.a6raywa1cher.rescheduletsuvk.models.UserInfo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInfoRepository extends CrudRepository<UserInfo, Integer> {

}
