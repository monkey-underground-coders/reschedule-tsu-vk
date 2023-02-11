package com.a6raywa1cher.rescheduletsuvk.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

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
