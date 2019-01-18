@PLUGIN@ - /projects/ REST API
==============================

This page describes the project related REST endpoints that are added
by the @PLUGIN@.

Please also take note of the general information on the
[REST API](../../../Documentation/rest-api.html).

<a id="project-endpoints"> Project Endpoints
--------------------------------------------

### <a id="delete-project"> Delete Project
_DELETE /projects/[\{project-name\}](../../../Documentation/rest-api-projects.html#project-name)_

OR

_POST /projects/[\{project-name\}](../../../Documentation/rest-api-projects.html#project-name)/@PLUGIN@~delete_

Deletes a project.

Options for the deletion can be specified in the request body as a
[DeleteOptionsInput](#delete-options-input) entity.

Please note that some proxies prohibit request bodies for _DELETE_
requests. In this case, if you want to specify options, use _POST_
to delete the project.

Caller must be a member of a group that is granted the 'Delete Project'
capability (provided by this plugin) or be a member of the Administrators
group.

#### Request

```
  DELETE /projects/MyProject HTTP/1.0
  Content-Type: application/json;charset=UTF-8

  {
    "force": true
  }
```

#### Response

```
  HTTP/1.1 204 No Content
```


<a id="json-entities">JSON Entities
-----------------------------------

### <a id="delete-options-info"></a>DeleteOptionsInfo

The `DeleteOptionsInfo` entity contains options for the deletion of a
project.

* _force_ (optional): If set the project is deleted even if it has open changes.
* _preserve_ (optional): If set the GIT repository of the project is not removed.

SEE ALSO
--------

* [Projects related REST endpoints](../../../Documentation/rest-api-projects.html)

GERRIT
------
Part of [Gerrit Code Review](../../../Documentation/index.html)
