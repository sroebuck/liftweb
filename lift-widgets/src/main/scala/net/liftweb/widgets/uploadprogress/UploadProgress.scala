package net.liftweb.widgets.uploadprogress

import _root_.scala.xml.{NodeSeq,Text}
import _root_.net.liftweb.http.{SessionVar,Req,GetRequest,PlainTextResponse,JsonResponse,
                                LiftRules,OnDiskFileParamHolder,S,ResourceServer}
import _root_.net.liftweb.http.js.JsCmds._
import _root_.net.liftweb.http.js.JE._
import _root_.net.liftweb.util.{Box,Empty,Failure,Full}

/**
 * A helper widget that makes it easy to do upload
 * progress bars using ajax polling.
 * 
 * @author Tim Perrett
 */
object UploadProgress {
  
  /**
   * Call UploadProgress.init from Boot.scala
   */ 
  def init = {
    /**
     * Enable file streaming uploads - this is required for progress updates
     */
    LiftRules.handleMimeFile = (fieldName, contentType, fileName, inputStream) =>
      OnDiskFileParamHolder(fieldName, contentType, fileName, inputStream)
    
    ResourceServer.allow({
      case "uploadprogress" :: "uploadprogress.js" :: Nil => true
    })
    
    LiftRules.dispatch.append {
      case Req("progress" :: Nil, "", GetRequest) => () => {
        Full(JsonResponse(
          JsObj("state" -> "uploading", 
                "received" -> Str(StatusHolder.is.map(_._1.toString).openOr("0")),
                "size" -> Str(StatusHolder.is.map(_._2.toString).openOr("0"))
          ))
        )
      }
    }
    
  }
  
  def sessionProgessListener =
    S.session.foreach(s => { 
      s.progessListener = Full((pBytesRead: Long, pBytesTotal: Long, pItem: Int) => {
        StatusHolder(Full((pBytesRead, pBytesTotal))) 
      })
    })
  
  def head(xhtml: NodeSeq): NodeSeq = {
    StatusHolder.is
    Script(Run(""" 
    $(function() {
      $('""" + S.attr("formId").openOr("form") + """').uploadProgress({
        start:function(){ },
        uploading: function(upload) {$('#percents').html(upload.percents+'%');},
        progressBar: '#""" + S.attr("progressBar").openOr("progressbar") + """',
        progressUrl: '""" + S.attr("progressUrl").openOr("/progress") + """',
        interval: """ + S.attr("interval").openOr("200") + """
      });
    });
    """))
  }
}

object StatusHolder extends SessionVar[Box[(Long, Long)]]({
  UploadProgress.sessionProgessListener
  Empty
})