package com.unimobili.agenda.user;

import com.unimobili.agenda.user.dto.CreateUserRequest;
import com.unimobili.agenda.user.dto.UpdateUserRequest;
import com.unimobili.agenda.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@Tag(name = "Usuários", description = "Gestão de usuários (somente GERENTE)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar usuário", description = "Cria um usuário com senha inicial (mín. 8). E-mail deve ser único.")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @GetMapping
    @Operation(summary = "Listar usuários", description = "Lista paginada de usuários.")
    public Page<UserResponse> list(Pageable pageable) {
        return userService.list(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhar usuário", description = "Retorna um usuário pelo id.")
    public UserResponse get(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário", description = "Atualiza dados do usuário (a senha não é alterada por aqui).")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desativar usuário", description = "Soft delete: marca o usuário como inativo.")
    public void delete(@PathVariable UUID id) {
        userService.softDelete(id);
    }
}
