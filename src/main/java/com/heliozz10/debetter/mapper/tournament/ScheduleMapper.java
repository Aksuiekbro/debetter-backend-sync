package com.heliozz10.debetter.mapper.tournament;

import com.heliozz10.debetter.content.tournament.Schedule;
import com.heliozz10.debetter.dto.tournament.in.ScheduleFormDto;
import com.heliozz10.debetter.dto.tournament.out.ScheduleView;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {
    Schedule toSchedule(ScheduleFormDto dto);

    ScheduleView toScheduleView(Schedule schedule);

    List<ScheduleView> toScheduleViews(List<Schedule> schedules);
}
