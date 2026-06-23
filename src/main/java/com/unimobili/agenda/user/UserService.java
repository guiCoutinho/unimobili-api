package com.unimobili.agenda.user;

import com.unimobili.agenda.user.dto.CreateUserRequest;
import com.unimobili.agenda.user.dto.UpdateUserRequest;
import com.unimobili.agenda.user.dto.UserResponse;
import com.unimobili.agenda.web.error.ConflictException;
import com.unimobili.agenda.web.error.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("E-mail já cadastrado: " + request.email());
        }

        User user = new User();
        user.setNome(request.nome());
        user.setEmail(request.email());
        user.setSenha(passwordEncoder.encode(request.senha()));
        user.setTelefone(request.telefone());
        user.setCargo(request.cargo());
        user.setRole(request.role());
        user.setAtivo(true);

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = findOrThrow(id);

        userRepository.findByEmail(request.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException("E-mail já cadastrado: " + request.email());
                });

        user.setNome(request.nome());
        user.setEmail(request.email());
        user.setTelefone(request.telefone());
        user.setCargo(request.cargo());
        user.setRole(request.role());
        user.setAtivo(request.ativo());

        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void softDelete(UUID id) {
        User user = findOrThrow(id);
        user.setAtivo(false);
        userRepository.save(user);
    }

    private User findOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
    }
}
