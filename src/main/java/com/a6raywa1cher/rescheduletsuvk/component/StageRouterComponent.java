package com.a6raywa1cher.rescheduletsuvk.component;

import com.a6raywa1cher.rescheduletsuvk.stages.PrimaryStage;
import com.a6raywa1cher.rescheduletsuvk.stages.Stage;
import com.petersamokhin.bots.sdk.clients.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StageRouterComponent {
	private final Group group;
	private final List<? extends Stage> stageList;
	private final PrimaryStage primaryStage;
	private final Map<Integer, Stage> integerStageMap;

	@Autowired
	public StageRouterComponent(Group group, List<? extends Stage> stageList, PrimaryStage primaryStage) {
		this.group = group;
		this.stageList = stageList;
		this.primaryStage = primaryStage;
		stageList.remove(primaryStage);
		integerStageMap = new ConcurrentHashMap<>();
	}

	public synchronized void link(Integer integer, Stage stage) {
		integerStageMap.put(integer, stage);
	}

	public synchronized void unlink(Integer integer) {
		integerStageMap.remove(integer);
	}

	@PostConstruct
	public void onStart() {
		group.onMessage(message -> {
			if (integerStageMap.containsKey(message.authorId())) {
				integerStageMap.get(message.authorId()).accept(message);
				return;
			}
			Optional<? extends Stage> anyStage = stageList.stream()
					.filter(stage -> stage.applicable(message))
					.findAny();
			if (anyStage.isPresent()) {
				anyStage.get().accept(message);
			} else {
				primaryStage.accept(message);
			}
		});
	}
}
