package com.heliozz10.debetter.dto.tournament.match.in;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor
public class MatchUpdateDto {
    @JsonIgnore
    private final Set<String> presentFields = new HashSet<>();

    @Size(max = 255)
    private String location;

    private LocalDateTime startTime;

    @Positive
    private Long judgeId;

    @Positive
    private Long team1Id;

    @Positive
    private Long team2Id;

    @Positive
    private Long team3Id;

    @Positive
    private Long team4Id;

    @Positive
    private Long debater1Id;

    @Positive
    private Long debater2Id;

    @JsonSetter("location")
    public void setLocation(String location) {
        presentFields.add("location");
        this.location = location;
    }

    @JsonSetter("startTime")
    public void setStartTime(LocalDateTime startTime) {
        presentFields.add("startTime");
        this.startTime = startTime;
    }

    @JsonSetter("judgeId")
    public void setJudgeId(Long judgeId) {
        presentFields.add("judgeId");
        this.judgeId = judgeId;
    }

    @JsonSetter("team1Id")
    public void setTeam1Id(Long team1Id) {
        presentFields.add("team1Id");
        this.team1Id = team1Id;
    }

    @JsonSetter("team2Id")
    public void setTeam2Id(Long team2Id) {
        presentFields.add("team2Id");
        this.team2Id = team2Id;
    }

    @JsonSetter("team3Id")
    public void setTeam3Id(Long team3Id) {
        presentFields.add("team3Id");
        this.team3Id = team3Id;
    }

    @JsonSetter("team4Id")
    public void setTeam4Id(Long team4Id) {
        presentFields.add("team4Id");
        this.team4Id = team4Id;
    }

    @JsonSetter("debater1Id")
    public void setDebater1Id(Long debater1Id) {
        presentFields.add("debater1Id");
        this.debater1Id = debater1Id;
    }

    @JsonSetter("debater2Id")
    public void setDebater2Id(Long debater2Id) {
        presentFields.add("debater2Id");
        this.debater2Id = debater2Id;
    }

    public boolean hasLocation() {
        return presentFields.contains("location");
    }

    public boolean hasStartTime() {
        return presentFields.contains("startTime");
    }

    public boolean hasJudgeId() {
        return presentFields.contains("judgeId");
    }

    public boolean hasTeam1Id() {
        return presentFields.contains("team1Id");
    }

    public boolean hasTeam2Id() {
        return presentFields.contains("team2Id");
    }

    public boolean hasTeam3Id() {
        return presentFields.contains("team3Id");
    }

    public boolean hasTeam4Id() {
        return presentFields.contains("team4Id");
    }

    public boolean hasDebater1Id() {
        return presentFields.contains("debater1Id");
    }

    public boolean hasDebater2Id() {
        return presentFields.contains("debater2Id");
    }
}
