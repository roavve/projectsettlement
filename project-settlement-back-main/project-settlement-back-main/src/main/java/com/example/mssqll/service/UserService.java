package com.example.mssqll.service;

import com.example.mssqll.dto.response.UserResponseDto;
import com.example.mssqll.dto.response.UserSearchDto;
import com.example.mssqll.models.User;
import java.util.List;
import java.util.Map;

public interface UserService {
    UserResponseDto updateUser(User user,Long id);
    UserResponseDto deleteUser(Long id);
    List<UserResponseDto> searchUsers(Map<String, Object> filters);}
