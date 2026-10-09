# language: es
Característica: Registro de usuarios en la API RESTful
  Como cliente de la API
  Quiero registrar nuevos usuarios con sus datos y teléfonos
  Para que puedan acceder al sistema de forma segura

  Escenario: Registro exitoso de usuario con teléfonos
    Dado que tengo los datos válidos de un usuario con nombre "Carlos Perez", correo "carlos@empresa.com", clave "Clave123" y teléfonos
    Cuando envío una solicitud POST a "/api/v1/users" con los datos del usuario
    Entonces el código de respuesta debe ser 201
    Y la respuesta debe contener un id generado en formato UUID
    Y la respuesta debe contener un token JWT no vacío
    Y el campo isactive debe ser verdadero
    Y los campos created, modified y last_login deben estar presentes

  Escenario: Error al registrar un usuario con correo duplicado
    Dado que ya existe un usuario registrado con correo "duplicado@empresa.com"
    Cuando intento registrar otro usuario con el mismo correo "duplicado@empresa.com" y clave "Clave123"
    Entonces el código de respuesta debe ser 409
    Y el mensaje de error debe ser "El correo ya registrado"

  Escenario: Error al registrar un usuario con formato de correo inválido
    Dado que tengo un usuario con correo inválido "correo-invalido" y clave "Clave123"
    Cuando envío una solicitud POST a "/api/v1/users" con los datos del usuario
    Entonces el código de respuesta debe ser 400
    Y el mensaje de error debe ser "El formato del correo es inválido"

  Escenario: Error al registrar un usuario con contraseña que no cumple la expresión regular
    Dado que tengo un usuario con correo "valido@empresa.com" y contraseña inválida "123"
    Cuando envío una solicitud POST a "/api/v1/users" con los datos del usuario
    Entonces el código de respuesta debe ser 400
    Y el mensaje de error debe ser "El formato de la contraseña es inválido"
