/*
 * Copyright 2020 Mamoe Technologies and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/mamoe/mirai/blob/master/LICENSE
 */

package net.mamoe.mirai.console.permission

import net.mamoe.mirai.console.data.PluginDataExtensions
import net.mamoe.mirai.console.permission.PermissibleIdentifier.Companion.grantedWith

/**
 *
 */
@ExperimentalPermission
internal abstract class AbstractConcurrentPermissionService<P : Permission> : PermissionService<P> {
    protected abstract val permissions: MutableMap<PermissionId, P>
    protected abstract val grantedPermissionsMap: PluginDataExtensions.NotNullMutableMap<PermissionId, MutableCollection<PermissibleIdentifier>>

    protected abstract fun createPermission(
        id: PermissionId,
        description: String,
        parent: Permission
    ): P

    override fun get(id: PermissionId): P? = permissions[id]

    override fun register(id: PermissionId, description: String, parent: Permission): P {
        val instance = createPermission(id, description, parent)
        val old = permissions.putIfAbsent(id, instance)
        if (old != null) throw DuplicatedPermissionRegistrationException(instance, old)
        return instance
    }

    override fun grant(permissibleIdentifier: PermissibleIdentifier, permission: P) {
        val id = permission.id
        grantedPermissionsMap[id].add(permissibleIdentifier)
    }

    override fun deny(permissibleIdentifier: PermissibleIdentifier, permission: P) {
        grantedPermissionsMap[permission.id].remove(permissibleIdentifier)
    }

    override fun getRegisteredPermissions(): Sequence<P> = permissions.values.asSequence()
    override fun getGrantedPermissions(permissibleIdentifier: PermissibleIdentifier): Sequence<P> = sequence<P> {
        for ((permissionIdentifier, permissibleIdentifiers) in grantedPermissionsMap) {

            val granted =
                if (permissibleIdentifiers.isEmpty()) false
                else permissibleIdentifiers.any { permissibleIdentifier.grantedWith(it) }

            if (granted) get(permissionIdentifier)?.let { yield(it) }
        }
    }
}