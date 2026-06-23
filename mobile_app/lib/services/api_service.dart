import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiService {
  // For macOS desktop, localhost works directly.
  // For iOS simulator, use 127.0.0.1. For Android emulator, use 10.0.2.2
  static const String baseUrl = 'http://localhost:8080/api/v1';

  /// Helper to convert relative server paths (like output_assets/...) to absolute URLs.
  static String getAbsoluteUrl(String url) {
    if (url.startsWith('http://') || url.startsWith('https://')) {
      return url;
    }
    if (url.startsWith('output_assets/')) {
      final hostUrl = baseUrl.replaceAll('/api/v1', '');
      return '$hostUrl/$url';
    }
    return url;
  }

  /// Fetch UGC video feed for the TikTok-style swipe screen.
  static Future<List<Map<String, dynamic>>> fetchFeed() async {
    try {
      final response = await http
          .get(Uri.parse('$baseUrl/ugc/feed'))
          .timeout(const Duration(seconds: 5));
      if (response.statusCode == 200) {
        final data = List<Map<String, dynamic>>.from(json.decode(response.body));
        if (data.isNotEmpty) {
          return data.map((item) {
            if (item['url'] != null) {
              item['url'] = getAbsoluteUrl(item['url'] as String);
            }
            return item;
          }).toList();
        }
      }
    } catch (e) {
      print('[ApiService] Feed fetch failed: $e — using mock data');
    }
    return _mockFeedData().map((item) {
      if (item['url'] != null) {
        item['url'] = getAbsoluteUrl(item['url'] as String);
      }
      return item;
    }).toList();
  }

  /// Fetch products for the Discover grid.
  static Future<List<Map<String, dynamic>>> fetchProducts() async {
    try {
      final response = await http
          .get(Uri.parse('$baseUrl/products/list'))
          .timeout(const Duration(seconds: 5));
      if (response.statusCode == 200) {
        final data = List<Map<String, dynamic>>.from(json.decode(response.body));
        if (data.isNotEmpty) {
          return data.map((item) {
            if (item['url'] != null) {
              item['url'] = getAbsoluteUrl(item['url'] as String);
            }
            return item;
          }).toList();
        }
      }
    } catch (e) {
      print('[ApiService] Products fetch failed: $e — using mock data');
    }
    return _mockFeedData().map((item) {
      if (item['url'] != null) {
        item['url'] = getAbsoluteUrl(item['url'] as String);
      }
      return item;
    }).toList();
  }

  /// Trigger video generation for a product via Higgsfield.
  static Future<bool> triggerVideoGeneration({
    required String productName,
    required String productImageUrl,
    String productDescription = '',
    String productType = 'fashion',
    String affiliateLink = '',
  }) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/ugc/generate'),
        headers: {'Content-Type': 'application/json'},
        body: json.encode({
          'productName': productName,
          'productImageUrl': productImageUrl,
          'productDescription': productDescription,
          'productType': productType,
          'affiliateLink': affiliateLink,
        }),
      );
      return response.statusCode == 200;
    } catch (e) {
      print('[ApiService] Video generation trigger failed: $e');
      return false;
    }
  }

  /// Fetch social auto-responder comments.
  static Future<List<Map<String, dynamic>>> fetchSocialComments() async {
    try {
      final response = await http
          .get(Uri.parse('$baseUrl/social/comments'))
          .timeout(const Duration(seconds: 5));
      if (response.statusCode == 200) {
        return List<Map<String, dynamic>>.from(json.decode(response.body));
      }
    } catch (e) {
      print('[ApiService] Social comments fetch failed: $e');
    }
    return [];
  }

  static List<Map<String, dynamic>> _mockFeedData() {
    return [
      {
        'id': 1,
        'url': 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=600',
        'affiliateLink': 'https://meshada.com/ref/123',
        'modelName': 'Aria',
        'itemName': 'Silk Wrap Dress',
        'createdAt': DateTime.now().toIso8601String(),
      },
      {
        'id': 2,
        'url': 'https://images.unsplash.com/photo-1529139574466-a303027c1d8b?w=600',
        'affiliateLink': 'https://meshada.com/ref/456',
        'modelName': 'Luna',
        'itemName': 'Leather Moto Jacket',
        'createdAt':
            DateTime.now().subtract(const Duration(hours: 1)).toIso8601String(),
      },
      {
        'id': 3,
        'url': 'https://images.unsplash.com/photo-1496747611176-843222e1e57c?w=600',
        'affiliateLink': 'https://meshada.com/ref/789',
        'modelName': 'Nova',
        'itemName': 'Oversized Blazer',
        'createdAt':
            DateTime.now().subtract(const Duration(hours: 2)).toIso8601String(),
      },
      {
        'id': 4,
        'url': 'https://images.unsplash.com/photo-1509631179647-0177331693ae?w=600',
        'affiliateLink': 'https://meshada.com/ref/101',
        'modelName': 'Aria',
        'itemName': 'Evening Gown',
        'createdAt':
            DateTime.now().subtract(const Duration(hours: 3)).toIso8601String(),
      },
      {
        'id': 5,
        'url': 'https://images.unsplash.com/photo-1485968579580-b6d095142e6e?w=600',
        'affiliateLink': 'https://meshada.com/ref/202',
        'modelName': 'Luna',
        'itemName': 'Denim Collection',
        'createdAt':
            DateTime.now().subtract(const Duration(hours: 4)).toIso8601String(),
      },
    ];
  }
}
