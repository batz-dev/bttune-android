/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * DiscordRpcRepositoryImpl.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.bt.bttune.discordrpc.repository

import com.bt.bttune.discordrpc.remote.ApiService
import com.bt.bttune.discordrpc.utils.toImageAsset

/**
 * Modified by Zion Huang
 */
class DiscordRpcRepository {
    private val api = ApiService()

    suspend fun getImage(url: String): String? {
        return api.getImage(url).getOrNull()?.toImageAsset()
    }
}
