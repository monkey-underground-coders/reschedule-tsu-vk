package com.a6raywa1cher.rescheduletsuvk.models;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
public class UserInfo {
	@Id
	private Integer peerId;

	@Column(nullable = false)
	private String facultyId;

	@Column(nullable = false)
	private String groupId;

	@Column
	private Integer subgroup;
}
