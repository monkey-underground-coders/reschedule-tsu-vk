package com.a6raywa1cher.rescheduletsuvk.dao;

import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Repository
public class UserInfoRepository {
	@PersistenceContext
	private EntityManager entityManager;
}
