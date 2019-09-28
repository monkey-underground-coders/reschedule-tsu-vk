package com.a6raywa1cher.rescheduletsuvk.filterstages;

import com.a6raywa1cher.rescheduletsuvk.component.ExtendedMessage;

public interface FilterStage {
	ExtendedMessage process(ExtendedMessage extendedMessage);
}
