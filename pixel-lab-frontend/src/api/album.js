/**
 * 【文件路径】src/api/album.js
 * 【功能说明】作品集相关接口
 */

import request from '@/utils/request'

export const createAlbum = (data) => request.post('/api/albums', data)
export const getAlbums = (params) => request.get('/api/albums', { params })
export const getAlbumDetail = (id) => request.get(`/api/albums/${id}`)
export const updateAlbum = (id, data) => request.patch(`/api/albums/${id}`, data)
export const deleteAlbum = (id) => request.delete(`/api/albums/${id}`)
export const addImageToAlbum = (albumId, data) => request.post(`/api/albums/${albumId}/images`, data)
export const removeImageFromAlbum = (albumId, imageId) => request.delete(`/api/albums/${albumId}/images/${imageId}`)
export const updateAlbumImageDescription = (albumId, imageId, description) => request.patch(`/api/albums/${albumId}/images/${imageId}`, { description })
export const reorderAlbumImages = (albumId, imageIds) => request.put(`/api/albums/${albumId}/reorder`, { imageIds })
