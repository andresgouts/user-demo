# language: es
Característica: Consulta de usuario por ID en la API RESTful
  Como cliente autenticado de la API
  Quiero consultar los datos de un usuario por su identificador único proporcionando un token JWT
  Para obtener su información de forma segura

  Escenario: Obtener usuario exitosamente con token JWT válido
    Dado que existe un usuario registrado en el sistema con correo "consulta_valida@empresa.com"
    Cuando solicito la información del usuario por su ID enviando el token JWT en la cabecera Authorization
    Entonces el código de respuesta debe ser 200
    Y la respuesta debe contener el id del usuario consultado
    Y la respuesta debe contener el nombre "Usuario Consulta" y correo "consulta_valida@empresa.com"
    Y la respuesta debe contener la lista de teléfonos del usuario
    Y el campo isactive debe ser verdadero

  Escenario: Error al consultar usuario sin cabecera Authorization
    Dado que existe un usuario registrado en el sistema con correo "sin_token@empresa.com"
    Cuando solicito la información del usuario por su ID sin cabecera Authorization
    Entonces el código de respuesta debe ser 401
    Y el mensaje de error debe ser "Token de autenticación no proporcionado"

  Escenario: Error al consultar usuario con token JWT inválido
    Dado que existe un usuario registrado en el sistema con correo "token_invalido@empresa.com"
    Cuando solicito la información del usuario por su ID con un token inválido "token.falso.invalido"
    Entonces el código de respuesta debe ser 401
    Y el mensaje de error debe ser "Token JWT inválido o expirado"

  Escenario: Error al consultar usuario con ID inexistente
    Dado que tengo un token JWT válido para "usuario@empresa.com"
    Cuando solicito un usuario con un ID inexistente "00000000-0000-0000-0000-000000000000" usando el token
    Entonces el código de respuesta debe ser 404
    Y el mensaje de error debe ser "Usuario no encontrado con id: 00000000-0000-0000-0000-000000000000"
