package com.guilherme.entrevistaia.dto;

// Corpo de PATCH /company/cor-marca (só ADMIN). "#rrggbb" (maiúsculas ou
// minúsculas); vazio ou null remove a cor. Validado no CompanyService.
public record CompanyColorRequest(String corMarca) {}
