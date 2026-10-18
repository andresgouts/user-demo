# language: es
Característica: Inicio de sesión de usuarios en la API RESTful
  Como usuario registrado en el sistema
  Quiero autenticarme con mi correo y contraseña
  Para obtener un token de acceso y consultar mi información

  Escenario: Inicio de sesión exitoso con credenciales correctas
    Dado que existe un usuario registrado en el sistema con correo "login_valido@empresa.com" y contraseña "Clave123"
    Cuando envío una solicitud POST a "/api/v1/auth/login" con el correo "login_valido@empresa.com" y contraseña "Clave123"
    Entonces el código de respuesta debe ser 200
    Y la respuesta debe contener un id generado en formato UUID
    Y la respuesta debe contener un token JWT no vacío
    Y la respuesta debe contener el nombre "Usuario Login" y correo "login_valido@empresa.com"
    Y la respuesta debe contener la lista de teléfonos del usuario
    Y el campo isactive debe ser verdadero
    Y los campos created, modified y last_login deben estar presentes

  Escenario: Error al iniciar sesión con contraseña incorrecta
    Dado que existe un usuario registrado en el sistema con correo "login_clave_mala@empresa.com" y contraseña "Clave123"
    Cuando envío una solicitud POST a "/api/v1/auth/login" con el correo "login_clave_mala@empresa.com" y contraseña "ClaveErronea999"
    Entonces el código de respuesta debe ser 401
    Y el mensaje de error debe ser "Credenciales inválidas"

  Escenario: Error al iniciar sesión con correo inexistente
    Dado que no existe ningún usuario registrado con el correo "no_existe_login@empresa.com"
    Cuando envío una solicitud POST a "/api/v1/auth/login" con el correo "no_existe_login@empresa.com" y contraseña "Clave123"
    Entonces el código de respuesta debe ser 401
    Y el mensaje de error debe ser "Credenciales inválidas"

  Escenario: Error al iniciar sesión con datos faltantes o vacíos
    Cuando envío una solicitud POST a "/api/v1/auth/login" con correo vacío y contraseña "Clave123"
    Entonces el código de respuesta debe ser 400
    Y el mensaje de error debe ser "El correo es obligatorio"
