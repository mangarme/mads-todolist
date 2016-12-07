package controllers;

import play.*;
import play.mvc.*;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.*;

import models.*;
import services.*;

import views.html.*;

import javax.inject.*;
import java.util.*;

public class ProyectosController extends Controller {
///despues de hacer un push con --force no me dejo reflejar los cambios, ahora hago esta modificacion
//para ver si se reflejan los cambios sin --force
    @Inject
    FormFactory formFactory, tareaFactory;
    List<Tarea> tareas = new ArrayList<Tarea>();
    List<Tarea> tareasProyecto = new ArrayList<Tarea>();

    @Transactional(readOnly = true)
    // Devuelve una página con la lista de proyectos
    public Result listaProyectos(Integer idUsuario) {


      String variable=session().get("usuario");
        if (variable!=null){
          Usuario usuario = UsuariosService.findUsuario(idUsuario);
            if (usuario == null) {
                return notFound("Usuario no encontrado");
            }else if(!usuario.login.equals(variable)){

                  return notFound("No autorizado a acceder a zonas de otros usuarios");
           } else {
                List<Proyecto> proyectos = ProyectosService.listaProyectosUsuario(idUsuario);
                return ok(listaProyectos.render(proyectos, usuario));
            }
        }else{

          return unauthorized("hello, debes iniciar session");
        }
    }


    // Devuelve un formulario para crear un nuevo proyecto
    @Transactional
    public Result formularioNuevoProyecto(Integer idUsuario) {
      Logger.debug("idddddd usuario: " + idUsuario);
      String variable=session().get("usuario");
        if (variable!=null){

            Usuario usuario = UsuariosService.findUsuario(idUsuario);
            if (usuario == null) {
                return notFound("Usuario no encontrado");
            }else if(!usuario.login.equals(variable)){

                  return notFound("No autorizado a acceder a zonas de otros usuarios");
            } else {
                  return ok(formCreacionProyecto.render(formFactory.form(Proyecto.class), idUsuario, ""));
            }
        }else{

          return unauthorized("hello, debes iniciar session");
        }
    }

    @Transactional
    // Añade una nuevp proyecto en la BD y devuelve código HTTP
    // de redirección a la página de listado de usuarios
    //PARA EJECUTAR ESTE MÉTODO HA SIDO NECESARIO REALIZAR: activator run
    //Daba error como si no detectara que el campo nombre del proyecto no existiera
    public Result grabaNuevoProyecto(Integer idUsuario) {

        Form<Proyecto> proyectoForm = formFactory.form(Proyecto.class).bindFromRequest();
        if (proyectoForm.hasErrors()) {
            return badRequest(formCreacionProyecto.render(proyectoForm, idUsuario, "Hay errores en el formulario"));
        }
        Proyecto proyecto = proyectoForm.get();


        proyecto = ProyectosService.crearProyectoUsuario(proyecto, idUsuario);
        flash("gestionaProyecto", "el proyecto se ha grabado correctamente");
        Logger.debug("Proyecto guardado correctamente: " + proyecto.toString());

        return redirect(routes.ProyectosController.listaProyectos(idUsuario));
    }

    @Transactional
    public Result formularioEditaProyecto(Integer idProyecto, Integer idUsuario) {
      String variable=session().get("usuario");
        if (variable!=null){
            //Cargamos vacío el form
            Form<Proyecto> proyectoForm = formFactory.form(Proyecto.class);
            //Obtenemos de la base de datos el proyecto
            //Proyecto proyecto = ProyectosService.findProyectoUsuario(idProyecto);  metodo original
            Proyecto proyecto = ProyectosService.findProyectoPorUsuario(idProyecto, idUsuario);
            //Cargamos en el form los datos del usuario
            proyectoForm = proyectoForm.fill(proyecto);

            //lineas añadidas

            tareas = ProyectosService.tareasNoAsignadas(idUsuario);
            tareasProyecto = proyecto.tareas;
            ////
            //Retornamos a la vista los datos del usuario en el form
            return ok(formModificacionProyecto.render(proyectoForm, tareas, tareasProyecto, idUsuario, ""));
        }else{
           return unauthorized("hello, debes iniciar session");

        }

    }

    @Transactional
    public Result grabaProyectoModificado(Integer idUsuario) {
        Form<Proyecto> proyectoForm = formFactory.form(Proyecto.class).bindFromRequest();


        if (proyectoForm.hasErrors()) {
            return badRequest(formModificacionProyecto.render(proyectoForm, tareas, tareasProyecto, idUsuario, "Hay errores en el formulario"));
        }

        //Recuperamos los datos de la proyecto
        Proyecto proyecto = proyectoForm.get();
        String id = Form.form().bindFromRequest().get("tareaDisponible");
        Logger.debug("iddddddddddddddd: " + id);
        Usuario usuario = UsuariosService.findUsuario(idUsuario);
        proyecto.usuario = usuario;
        if (id != null) {

            //Comprobamos que el usuario existe (evitamos problemas de referencias)


            Tarea tarea = TareasService.findTareaUsuario(Integer.parseInt(id));
            Logger.debug("tareaaaaaaaaa: " + tarea);
            if (usuario != null) {

                proyecto.usuario = usuario;
                tarea.proyecto = proyecto;
                //proyectoForm.get().tareaDisponible.value;
                Logger.debug("proyecto guardada correctamente (modificar): " + proyecto.toString());
                proyecto.tareas.add(tarea);

                proyecto = ProyectosService.modificaProyectoUsuario(proyecto);
                proyecto.nombre = Form.form().bindFromRequest().get("nombre");
                flash("gestionaproyecto", "La proyecto se ha modificado correctamente (modificar)");
                Logger.debug("proyecto guardada correctamente (modificar): " + proyecto.toString());
                return redirect(routes.ProyectosController.formularioEditaProyecto(proyecto.id, idUsuario));
            } else {
                return badRequest(formModificacionProyecto.render(proyectoForm, tareas, tareasProyecto, idUsuario, "Error inesperado. Vuelva a intentarlo"));
            }
        }
        Logger.debug("iddddddddddddddd  not null: " + id);

        proyecto.nombre = Form.form().bindFromRequest().get("nombre");
        flash("gestionaproyecto", "La proyecto se ha modificado correctamente (modificar)");
        Logger.debug("proyecto guardada correctamente (modificar): " + proyecto.toString());
        return redirect(routes.ProyectosController.formularioEditaProyecto(proyecto.id, idUsuario));
        //return ok();
        //return badRequest(formModificacionProyecto.render(proyectoForm,tareas,tareasProyecto, idUsuario, "Error inesperado. Vuelva a intentarlo"));


    }

    @Transactional
    public Result deleteTareaDeProyecto(int idUsuario, int idTarea, int idProyecto) {

      String variable=session().get("usuario");
        if (variable!=null){
            //Si se ha borrado recargamos página
            if (ProyectosService.deleteTarea(idUsuario, idTarea, idProyecto)) {
                return ok("Tarea borrada con éxito.");
            } else { //Si no, devolvemos error
                return badRequest("Tarea no se ha podido eliminar.");
            }
        }else{
          return unauthorized("hello, debes iniciar session");
        }

    }

    @Transactional
    public Result borraProyecto(int idProyecto, int idUsuario) {

      String variable=session().get("usuario");
        if (variable!=null){
            Proyecto proyecto = ProyectosService.findProyectoUsuario(idProyecto);
            //Si se ha borrado recargamos página
            if (ProyectosService.deleteProyecto(idProyecto)) {
                return ok("Proyecto borrado con éxito.");
            } else { //Si no, devolvemos error
                return badRequest("Proyecto no se ha podido eliminar.");
            }
        }else{

            return unauthorized("hello, debes iniciar session");
        }
    }

    @Transactional
    public Result detalleProyecto(int idProyecto, int idUsuario) {
      String variable=session().get("usuario");
        if (variable!=null){
            Proyecto proyecto = ProyectosService.findProyectoPorUsuario(idProyecto, idUsuario);
              return ok(detalleProyecto.render(proyecto));
        }else{
          return unauthorized("hello, debes iniciar session");
        }
    }


}
