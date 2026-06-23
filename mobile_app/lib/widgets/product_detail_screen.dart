import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:url_launcher/url_launcher.dart';
import '../theme/app_theme.dart';

class ProductDetailScreen extends StatefulWidget {
  final Map<String, dynamic> product;
  final String imageUrl;

  const ProductDetailScreen({
    super.key,
    required this.product,
    required this.imageUrl,
  });

  @override
  State<ProductDetailScreen> createState() => _ProductDetailScreenState();
}

class _ProductDetailScreenState extends State<ProductDetailScreen> {
  int _currentImageIndex = 0;
  bool _isSaved = false;
  bool _isLiked = false;

  // Multiple images for the gallery
  late final List<String> _images;

  @override
  void initState() {
    super.initState();
    _images = [
      widget.imageUrl,
      'https://images.unsplash.com/photo-1581044777550-4cfa60707998?w=600',
      'https://images.unsplash.com/photo-1509631179647-0177331693ae?w=600',
    ];
  }

  @override
  Widget build(BuildContext context) {
    final itemName = widget.product['itemName'] ?? 'Fashion Item';
    final modelName = widget.product['modelName'] ?? 'Meshada';
    final affiliateLink = widget.product['affiliateLink'] ?? '';

    return Scaffold(
      backgroundColor: AppTheme.scaffoldBg,
      body: CustomScrollView(
        slivers: [
          // Image gallery with parallax
          SliverAppBar(
            expandedHeight: MediaQuery.of(context).size.height * 0.55,
            pinned: true,
            backgroundColor: AppTheme.scaffoldBg,
            leading: GestureDetector(
              onTap: () => Navigator.pop(context),
              child: Container(
                margin: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.black.withValues(alpha: 0.4),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.arrow_back_rounded,
                    color: Colors.white, size: 20),
              ),
            ),
            actions: [
              GestureDetector(
                onTap: () => setState(() => _isSaved = !_isSaved),
                child: Container(
                  margin: const EdgeInsets.all(8),
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: Colors.black.withValues(alpha: 0.4),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    _isSaved
                        ? Icons.bookmark_rounded
                        : Icons.bookmark_border_rounded,
                    color: _isSaved ? AppTheme.warmGold : Colors.white,
                    size: 20,
                  ),
                ),
              ),
              GestureDetector(
                onTap: () {},
                child: Container(
                  margin: const EdgeInsets.only(right: 16, top: 8, bottom: 8),
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: Colors.black.withValues(alpha: 0.4),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.share_outlined,
                      color: Colors.white, size: 20),
                ),
              ),
            ],
            flexibleSpace: FlexibleSpaceBar(
              background: Stack(
                children: [
                  // Image carousel
                  PageView.builder(
                    itemCount: _images.length,
                    onPageChanged: (i) =>
                        setState(() => _currentImageIndex = i),
                    itemBuilder: (context, index) {
                      return CachedNetworkImage(
                        imageUrl: _images[index],
                        fit: BoxFit.cover,
                        width: double.infinity,
                        errorWidget: (_, __, ___) => Container(
                          color: AppTheme.cardBg,
                          child: const Center(
                            child: Icon(Icons.broken_image_outlined,
                                color: AppTheme.muted, size: 48),
                          ),
                        ),
                      );
                    },
                  ),

                  // Page indicator
                  Positioned(
                    bottom: 20,
                    left: 0,
                    right: 0,
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: List.generate(_images.length, (i) {
                        final isActive = i == _currentImageIndex;
                        return AnimatedContainer(
                          duration: const Duration(milliseconds: 250),
                          width: isActive ? 24 : 8,
                          height: 4,
                          margin: const EdgeInsets.symmetric(horizontal: 3),
                          decoration: BoxDecoration(
                            color: isActive
                                ? AppTheme.warmCream
                                : Colors.white.withValues(alpha: 0.3),
                            borderRadius: BorderRadius.circular(2),
                          ),
                        );
                      }),
                    ),
                  ),
                ],
              ),
            ),
          ),

          // Product details
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(24, 28, 24, 0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Brand + AI badge
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 10, vertical: 5),
                        decoration: BoxDecoration(
                          color: AppTheme.warmGold.withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          'AI CURATED',
                          style: GoogleFonts.dmSans(
                            color: AppTheme.warmGold,
                            fontSize: 10,
                            fontWeight: FontWeight.w700,
                            letterSpacing: 1.5,
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Text(
                        'Styled by $modelName',
                        style: const TextStyle(
                          color: AppTheme.muted,
                          fontSize: 13,
                        ),
                      ),
                    ],
                  ),

                  const SizedBox(height: 16),

                  // Title
                  Text(
                    itemName,
                    style: GoogleFonts.dmSans(
                      color: AppTheme.warmCream,
                      fontSize: 28,
                      fontWeight: FontWeight.w400,
                      letterSpacing: -0.8,
                      height: 1.2,
                    ),
                  ),

                  const SizedBox(height: 24),

                  // Interaction row
                  Row(
                    children: [
                      GestureDetector(
                        onTap: () => setState(() => _isLiked = !_isLiked),
                        child: Row(
                          children: [
                            Icon(
                              _isLiked
                                  ? Icons.favorite_rounded
                                  : Icons.favorite_border_rounded,
                              color: _isLiked
                                  ? AppTheme.softRose
                                  : AppTheme.muted,
                              size: 22,
                            ),
                            const SizedBox(width: 6),
                            Text(
                              _isLiked ? '2.5k' : '2.4k',
                              style: TextStyle(
                                color: _isLiked
                                    ? AppTheme.softRose
                                    : AppTheme.muted,
                                fontSize: 13,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 24),
                      Row(
                        children: [
                          Icon(Icons.remove_red_eye_outlined,
                              color: AppTheme.muted.withValues(alpha: 0.6),
                              size: 20),
                          const SizedBox(width: 6),
                          const Text(
                            '12.3k views',
                            style:
                                TextStyle(color: AppTheme.muted, fontSize: 13),
                          ),
                        ],
                      ),
                    ],
                  ),

                  const SizedBox(height: 32),

                  // Divider
                  Container(
                    height: 1,
                    color: Colors.white.withValues(alpha: 0.06),
                  ),

                  const SizedBox(height: 28),

                  // Description
                  Text(
                    'About this piece',
                    style: GoogleFonts.dmSans(
                      color: AppTheme.warmCream,
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'Handpicked by our AI styling engine and featured by $modelName. This piece exemplifies modern elegance with clean lines and a versatile silhouette that transitions effortlessly from day to evening.',
                    style: TextStyle(
                      color: AppTheme.muted.withValues(alpha: 0.8),
                      fontSize: 14,
                      height: 1.6,
                    ),
                  ),

                  const SizedBox(height: 28),

                  // Features
                  Row(
                    children: [
                      _FeatureItem(
                        icon: Icons.verified_outlined,
                        label: 'Verified\nAuthentic',
                      ),
                      const SizedBox(width: 12),
                      _FeatureItem(
                        icon: Icons.local_shipping_outlined,
                        label: 'Free\nShipping',
                      ),
                      const SizedBox(width: 12),
                      _FeatureItem(
                        icon: Icons.autorenew_rounded,
                        label: 'Easy\nReturns',
                      ),
                      const SizedBox(width: 12),
                      _FeatureItem(
                        icon: Icons.percent_rounded,
                        label: 'Earn\nCashback',
                      ),
                    ],
                  ),

                  const SizedBox(height: 120),
                ],
              ),
            ),
          ),
        ],
      ),

      // Sticky bottom CTA
      bottomNavigationBar: Container(
        padding: EdgeInsets.fromLTRB(
            24, 16, 24, MediaQuery.of(context).padding.bottom + 16),
        decoration: BoxDecoration(
          color: AppTheme.scaffoldBg,
          border: Border(
            top: BorderSide(color: Colors.white.withValues(alpha: 0.06)),
          ),
        ),
        child: GestureDetector(
          onTap: () async {
            final url = Uri.parse(affiliateLink);
            if (await canLaunchUrl(url)) {
              launchUrl(url, mode: LaunchMode.externalApplication);
            }
          },
          child: Container(
            padding: const EdgeInsets.symmetric(vertical: 18),
            decoration: BoxDecoration(
              color: AppTheme.warmCream,
              borderRadius: BorderRadius.circular(14),
            ),
            child: Center(
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.shopping_bag_outlined,
                      color: AppTheme.scaffoldBg, size: 22),
                  const SizedBox(width: 10),
                  Text(
                    'Shop This Look',
                    style: GoogleFonts.dmSans(
                      color: AppTheme.scaffoldBg,
                      fontWeight: FontWeight.w700,
                      fontSize: 17,
                      letterSpacing: 0.2,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _FeatureItem extends StatelessWidget {
  final IconData icon;
  final String label;

  const _FeatureItem({required this.icon, required this.label});

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 16),
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.03),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: Colors.white.withValues(alpha: 0.05)),
        ),
        child: Column(
          children: [
            Icon(icon, color: AppTheme.warmGold, size: 22),
            const SizedBox(height: 8),
            Text(
              label,
              textAlign: TextAlign.center,
              style: GoogleFonts.dmSans(
                color: AppTheme.muted,
                fontSize: 11,
                fontWeight: FontWeight.w500,
                height: 1.3,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
