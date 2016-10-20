package controllers;

import java.util.List;
import javax.inject.*;

import play.*;
import play.mvc.*;
import views.html.*;
import static play.libs.Json.*;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.*;

import services.*;
import models.*;

public class UsuariosController extends Controller {

   @Inject FormFactory formFactory;

   @Transactional(readOnly = true)
    // Devuelve una página con la lista de usuarios
    public Result listaUsuarios() {
        // Obtenemos el mensaje flash guardado en la petición
        // por el controller grabaUsuario
        String mensaje = flash("gestionaUsuario");
        List<Usuario> usuarios = UsuariosService.findAllUsuarios();
        return ok(listaUsuarios.render(usuarios, mensaje));
    }


  // Devuelve un formulario para crear un nuevo usuario
   public Result formularioNuevoUsuario() {
       return ok(formCreacionUsuario.render(formFactory.form(Usuario.class),""));
   }

   @Transactional
   // Añade un nuevo usuario en la BD y devuelve código HTTP
   // de redirección a la página de listado de usuarios
   public Result grabaNuevoUsuario() {

       Form<Usuario> usuarioForm = formFactory.form(Usuario.class).bindFromRequest();
       if (usuarioForm.hasErrors()) {
           return badRequest(formCreacionUsuario.render(usuarioForm, "Hay errores en el formulario"));
       }
       Usuario usuario = usuarioForm.get();
       Logger.debug("Usuario a grabar: " + usuario.toString());
       usuario = UsuariosService.grabaUsuario(usuario);
       flash("gestionaUsuario", "El usuario se ha grabado correctamente");
       Logger.debug("Usuario guardado correctamente: " + usuario.toString());
       return redirect(controllers.routes.UsuariosController.listaUsuarios());

     }

    @Transactional
    public Result detalleUsuario(int id) {
      Usuario usuario = UsuariosService.findUsuario(id);
      return ok(detalleUsuario.render(usuario));
    }

    @Transactional
    public Result grabaUsuarioModificado() {
      Form<Usuario> usuarioForm = formFactory.form(Usuario.class).bindFromRequest();
      if (usuarioForm.hasErrors()) {
          return badRequest(formModificacionUsuario.render(usuarioForm, "Hay errores en el formulario"));
      }
      Usuario usuario = usuarioForm.get();
      Logger.debug("Usuario a grabar: " + usuario.toString());
      usuario = UsuariosService.modificaUsuario(usuario);
      flash("gestionaUsuario", "El usuario se ha modificado correctamente (modificar)");
      Logger.debug("Usuario guardado correctamente (modificar): " + usuario.toString());
      return redirect(controllers.routes.UsuariosController.listaUsuarios());
    }

    @Transactional
    public Result editaUsuario(int id) {
        //Cargamos vacío el form
        Form<Usuario> usuarioForm = formFactory.form(Usuario.class);
        //Obtenemos de la base de datos el usuario
        Usuario usuario = UsuariosService.findUsuario(id);
        //Cargamos en el form los datos del usuario
        usuarioForm = usuarioForm.fill(usuario);
        //Retornamos a la vista los datos del usuario en el form
        return ok(formModificacionUsuario.render(usuarioForm, ""));
    }

    /**
     * Correción:
     * TIC-17 - UsuariosService.deleteUsuario(id) ya devuelve TRUE o FALSE dependiendo de si se ha borrado
     * o no. Gracias a las pruebas se ha detectado el mal funcionamiento.
     * Devolvemos respuesta al AJAX, que a su vez, después de mostrar el mensaje, recargará la página
     * @param id
     * @return Result
     */
    @Transactional
    public Result borraUsuario(int id) {
        //Si se ha borrado recargamos página
        if(UsuariosService.deleteUsuario(id)){
            return ok("Usuario borrado con éxito.");
        }
        else{ //Si no, devolvemos error
            return badRequest("Usuario no se ha podido eliminar.");
        }

    }
}
