package com.a6raywa1cher.rescheduletsuvk.models;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
public class TeacherUser implements UserInfo {
	@Id
	private Integer peerId;

	@Column
	private String teacherName;
}
