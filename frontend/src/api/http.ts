import axios from 'axios'
import type { ApiResponse } from '@/types/domain'

export const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>
    if (payload && payload.code !== 0) {
      return Promise.reject(new Error(payload.message || '请求处理失败'))
    }
    return response
  },
  (error) => {
    const message = error?.response?.data?.message || error?.message || '网络请求失败'
    return Promise.reject(new Error(message))
  },
)

export async function getData<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const response = await http.get<ApiResponse<T>>(url, { params })
  return response.data.data
}

export async function postData<T>(url: string, data?: unknown): Promise<T> {
  const response = await http.post<ApiResponse<T>>(url, data)
  return response.data.data
}

export async function putData<T>(url: string, data?: unknown): Promise<T> {
  const response = await http.put<ApiResponse<T>>(url, data)
  return response.data.data
}

export async function deleteData<T>(url: string): Promise<T> {
  const response = await http.delete<ApiResponse<T>>(url)
  return response.data.data
}
