# Permissions

This documentation outlines the various permissions required across different areas of the Dulno Panel. Permissions follow the schema **`<Scope>.<Action>`**.

---

## Table of Contents
1. [Asterisk](#asterisk)
2. [Tickets](#tickets)
3. [Questions](#questions)
4. [Sales](#sales)
5. [Users](#users)
6. [Organizations](#organizations)
7. [Entities](#entities)
8. [Offers](#offers)
9. [Templates](#templates)
10. [Maintenance](#maintenance)
11. [Cluster](#cluster)
12. [Groups](#groups)
13. [Members](#members)

---

## 1. Asterisk

The asterisk permission (*) can be understood as a wildcard. Whether member or group: every entity with the asterisk authorization has no restrictions and has all authorizations.

---

## 2. Tickets

The Tickets section contains permissions required for listing, viewing, answering, and closing tickets.

| Permission              | Description                          |
|-------------------------|--------------------------------------|
| `tickets.find`          | Allows listing all pending tickets.  |
| `ticket.find`           | Allows viewing an individual ticket. |
| `ticket.answer`         | Allows answering a ticket.           |
| `ticket.message.delete` | Allows deleting a ticket message.    |
| `ticket.close`          | Allows closing a ticket.             |

---

## 3. Questions

The Questions section contains permissions required for listing, viewing, answering, and closing questions.

| Permission        | Description                            |
|-------------------|----------------------------------------|
| `questions.find`  | Allows listing all pending questions.  |
| `question.find`   | Allows viewing an individual question. |
| `question.answer` | Allows answering a question.           |
| `question.close`  | Allows closing a question.             |

---

## 4. Sales

The Sales section contains permissions required for listing, viewing, answering, and closing sales.

| Permission    | Description                        |
|---------------|------------------------------------|
| `sales.find`  | Allows listing all pending sales.  |
| `sale.find`   | Allows viewing an individual sale. |
| `sale.answer` | Allows answering a sale.           |
| `sale.close`  | Allows closing a sale.             |

---

## 5. Users

The Users section contains permissions required for searching and viewing users, viewing and resending user mails and viewing and closing user sessions.

| Permission      | Description                                     |
|-----------------|-------------------------------------------------|
| `user.search`   | Allows searching for an user.                   |
| `user.find`     | Allows viewing an individual user.              |
| `mails.find`    | Allows viewing all mails send to the user.      |
| `mail.resend`   | Allows resending a mail to the user.            |
| `sessions.find` | Allows viewing all active sessions of the user. |
| `session.close` | Allows closing an user session.                 |

---

## 6. Organizations

The Organizations section contains permissions required for viewing and creating organizations.

| Permission            | Description                                |
|-----------------------|--------------------------------------------|
| `organization.find`   | Allows viewing an individual organization. |
| `organization.create` | Allows creating a new organization.        |

---

## 7. Entities

The Entities section contains permissions required for viewing and editing the bundle and payments of the entities.

| Permission                  | Description                                            |
|-----------------------------|--------------------------------------------------------|
| `entity.bundle.find`        | Allows viewing the bundle of an entity.                |
| `entity.bundle.change`      | Allows changing the bundle of an entity.               |
| `entity.bundle.delete`      | Allows deleting the bundle of an entity.               |
| `entity.payments.find`      | Allows viewing the payments of an entity.              |
| `entity.termination.status` | Allows retrieving the termination status of an entity. |
| `entity.bundle.terminate`   | Allows terminate the bundle of an entity.              |

---

## 8. Offers

The Offers section contains permissions required for listing, viewing, creating and withdrawing offers.

| Permission               | Description                                        |
|--------------------------|----------------------------------------------------|
| `offers.find`            | Allows listing all offers of an entity.            |
| `offer.find`             | Allows viewing an individual offer.                |
| `offer.price.suggestion` | Allows generating a price suggestion for an offer. |
| `offer.create`           | Allows creating a new offer.                       |
| `offer.withdraw`         | Allows withdrawing an offer.                       |

---

## 9. Templates

The Templates section contains permissions required for creating, updating and deleting templates.

| Permission        | Description                 |
|-------------------|-----------------------------|
| `template.create` | Allows creating a template. |
| `template.update` | Allows updating a template. |
| `template.delete` | Allows deleting a template. |

---

## 10. Maintenance

The Maintenance section contains permissions required for listing, creating, finishing and deleting maintenance.

| Permission           | Description                                        |
|----------------------|----------------------------------------------------|
| `maintenance.find`   | Allows listing all pending and active maintenance. |
| `maintenance.create` | Allows creating a maintenance.                     |
| `maintenance.finish` | Allows finishing a maintenance.                    |
| `maintenance.delete` | Allows deleting a maintenance.                     |

## 11. Cluster

The Cluster section contains permissions required for monitoring and editing the state of the cluster.

| Permission                 | Description                                       |
|----------------------------|---------------------------------------------------|
| `cluster.resources`        | Allows viewing the used resources of the cluster. |
| `cluster.servers`          | Allows listing all servers of the cluster.        |
| `cluster.server`           | Allows viewing an individual cluster server.      |
| `cluster.units`            | Allows listing all units of the cluster.          |
| `cluster.unit`             | Allows viewing an individual cluster unit.        |
| `cluster.unit.restart`     | Allows restarting an individual cluster unit.     |
| `cluster.unit.scale`       | Allows scaling an individual cluster unit.        |
| `cluster.pod`              | Allows viewing an individual cluster pod.         |
| `cluster.pod.delete`       | Allows deleting an individual cluster pod.        |
| `cluster.backups`          | Allows listing all backups of the cluster.        |
| `cluster.backup.download`  | Allows downloading an individual cluster backup.  |
| `cluster.backup.upload`    | Allows uploading an individual cluster backup.    |
| `cluster.backup.restore`   | Allows restoring an individual cluster backup.    |
| `cluster.backup.delete`    | Allows deleting an individual cluster backup.     |
| `cluster.errors`           | Allows listing all errors of the cluster.         |
| `cluster.error.fixed`      | Allows fixing an individual cluster error.        |
| `cluster.errors.fixed.all` | Allows fixing all errors of the cluster.          |

---

## 12. Groups

The Groups section contains permissions required for listing, adding, removing, renaming and editing groups.

| Permission                | Description                                |
|---------------------------|--------------------------------------------|
| `groups.find`             | Allows listing all groups.                 |
| `group.add`               | Allows adding a new group.                 |
| `group.remove`            | Allows removing a group.                   |
| `group.rename`            | Allows renaming a group.                   |
| `group.priority.change`   | Allows changing the priority of a group.   |
| `group.color.change`      | Allows changing the color of a group.      |
| `group.permission.add`    | Allows adding a permission to a group.     |
| `group.permission.remove` | Allows removing a permission from a group. |

---

## 13. Members

The Members section contains permissions required for listing, adding, removing and editing groups.

| Permission                  | Description                                 |
|-----------------------------|---------------------------------------------|
| `members.find`              | Allows listing all members.                 |
| `members.add`               | Allows adding a new member.                 |
| `members.remove`            | Allows removing a members.                  |
| `members.group.change`      | Allows changing the group of a member.      |
| `members.permission.add`    | Allows adding a permission to a member.     |
| `members.permission.remove` | Allows removing a permission from a member. |

---

## Notes

- **Fine-grained Access Control**: Depending on specific needs, a more detailed permission configuration may be required for certain roles.
- **Least Privilege Principle**: Permissions should be granted according to the **principle of least privilege**, ensuring users have only the access they need to perform their tasks.

---