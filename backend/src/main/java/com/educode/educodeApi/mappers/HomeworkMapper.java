package com.educode.educodeApi.mappers;

import com.educode.educodeApi.DTO.homework.HomeworkCreateDTO;
import com.educode.educodeApi.DTO.homework.HomeworkUpdateDTO;
import com.educode.educodeApi.DTO.homework.HomeworkViewDTO;
import com.educode.educodeApi.models.Group;
import com.educode.educodeApi.models.Homework;
import com.educode.educodeApi.models.Puzzle;
import com.educode.educodeApi.utils.StdOptional;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class HomeworkMapper {

    /**
     * Создает объект Homework из DTO для создания
     * @param dto DTO с данными для создания
     * @param group группа для домашнего задания
     * @param puzzle задача (может быть null)
     * @return новый объект Homework
     */
    public Homework fromDTO(HomeworkCreateDTO dto, Group group, Puzzle puzzle) {
        Homework homework = new Homework();
        homework.setTitle(dto.title());
        homework.setContent(dto.content());
        homework.setGroup(group);
        homework.setPuzzle(puzzle);
        homework.setDeadline(dto.deadline());
        return homework;
    }

    /**
     * Обновляет объект Homework данными из DTO
     * @param homework обновляемый объект
     * @param dto DTO с новыми данными
     * @param puzzle задача (может быть null)
     */
    public void setFromUpdateDTO(Homework homework, HomeworkUpdateDTO dto, Puzzle puzzle) {
        homework.setTitle(dto.title());
        homework.setContent(dto.content());
        homework.setPuzzle(puzzle);
        homework.setDeadline(dto.deadline());
    }

    /**
     * Преобразует объект Homework в DTO для просмотра
     * @param homework объект Homework
     * @return DTO для просмотра
     */
    public HomeworkViewDTO toViewDTO(Homework homework, StdOptional<Group> groupOptional) {
        if (homework == null) return null;

        Group group = groupOptional.orElseGet(homework::getGroup);

        return new HomeworkViewDTO(
                homework.getId(),
                homework.getTitle(),
                homework.getContent(),
                group != null ? group.getId() : null,
                group != null ? group.getTitle() : null,
                homework.getPuzzle() != null ? homework.getPuzzle().getId() : null,
                homework.getPuzzle() != null ? homework.getPuzzle().getTitle() : null,
                homework.getCreatedAt(),
                homework.getUpdatedAt(),
                homework.getDeadline(),
                false, // будет установлено отдельно
                0,     // будет установлено отдельно
                isExpired(homework.getDeadline())
        );
    }

    public HomeworkViewDTO toViewDTO(Homework homework) {
        return toViewDTO(homework, StdOptional.empty());
    }

    /**
     * Обогащает DTO информацией о сдаче задания
     * @param dto базовый DTO
     * @param hasSubmitted сдал ли пользователь задание
     * @return обогащенный DTO
     */
    public HomeworkViewDTO enrichWithSubmissionInfo(HomeworkViewDTO dto, boolean hasSubmitted) {
        return new HomeworkViewDTO(
                dto.id(),
                dto.title(),
                dto.content(),
                dto.groupId(),
                dto.groupTitle(),
                dto.puzzleId(),
                dto.puzzleTitle(),
                dto.createdAt(),
                dto.updatedAt(),
                dto.deadline(),
                hasSubmitted,
                dto.submissionsCount(),
                dto.isExpired()
        );
    }

    /**
     * Обогащает DTO информацией о количестве сданных работ
     * @param dto базовый DTO
     * @param submissionsCount количество сданных работ
     * @return обогащенный DTO
     */
    public HomeworkViewDTO enrichWithSubmissionsCount(HomeworkViewDTO dto, long submissionsCount) {
        return new HomeworkViewDTO(
                dto.id(),
                dto.title(),
                dto.content(),
                dto.groupId(),
                dto.groupTitle(),
                dto.puzzleId(),
                dto.puzzleTitle(),
                dto.createdAt(),
                dto.updatedAt(),
                dto.deadline(),
                dto.hasSubmitted(),
                submissionsCount,
                dto.isExpired()
        );
    }

    /**
     * Проверяет, истек ли срок сдачи домашнего задания
     * @param deadline срок сдачи
     * @return true если срок истек
     */
    private boolean isExpired(LocalDateTime deadline) {
        return deadline != null && deadline.isBefore(LocalDateTime.now());
    }
}
