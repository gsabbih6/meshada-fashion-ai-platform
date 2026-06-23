import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:url_launcher/url_launcher.dart';
import '../theme/app_theme.dart';

class ProductBottomSheet extends StatelessWidget {
  final Map<String, dynamic> videoData;

  const ProductBottomSheet({super.key, required this.videoData});

  @override
  Widget build(BuildContext context) {
    final itemName = videoData['itemName'] ?? 'Fashion Item';
    final modelName = videoData['modelName'] ?? 'Model';
    final affiliateLink = videoData['affiliateLink'] ?? '';

    return Container(
      decoration: const BoxDecoration(
        color: AppTheme.cardBg,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // Handle
          Padding(
            padding: const EdgeInsets.only(top: 12),
            child: Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.white.withValues(alpha: 0.15),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),

          Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Header
                Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            itemName,
                            style: GoogleFonts.dmSans(
                              color: AppTheme.warmCream,
                              fontSize: 22,
                              fontWeight: FontWeight.w600,
                              letterSpacing: -0.4,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'Styled by $modelName',
                            style: const TextStyle(
                              color: AppTheme.warmGold,
                              fontSize: 14,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(
                            color: Colors.white.withValues(alpha: 0.1)),
                      ),
                      child: const Icon(Icons.bookmark_border_rounded,
                          color: AppTheme.warmCream, size: 20),
                    ),
                  ],
                ),

                const SizedBox(height: 24),

                // Details grid
                Row(
                  children: [
                    _DetailChip(icon: Icons.verified_outlined, label: 'Authentic'),
                    const SizedBox(width: 8),
                    _DetailChip(
                        icon: Icons.local_shipping_outlined, label: 'Free Ship'),
                    const SizedBox(width: 8),
                    _DetailChip(icon: Icons.percent_rounded, label: 'Cashback'),
                  ],
                ),

                const SizedBox(height: 24),

                // Description
                Text(
                  'This piece has been curated by our AI stylist and verified for authenticity. Click below to shop this exact look through our partner retailer.',
                  style: TextStyle(
                    color: AppTheme.muted.withValues(alpha: 0.8),
                    fontSize: 14,
                    height: 1.5,
                  ),
                ),

                const SizedBox(height: 28),

                // CTA Button
                SizedBox(
                  width: double.infinity,
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
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Center(
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(Icons.shopping_bag_outlined,
                                color: AppTheme.scaffoldBg, size: 20),
                            const SizedBox(width: 10),
                            Text(
                              'Shop This Look',
                              style: GoogleFonts.dmSans(
                                color: AppTheme.scaffoldBg,
                                fontWeight: FontWeight.w700,
                                fontSize: 16,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),

                SizedBox(height: MediaQuery.of(context).padding.bottom + 8),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _DetailChip extends StatelessWidget {
  final IconData icon;
  final String label;

  const _DetailChip({required this.icon, required this.label});

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12),
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.04),
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: Colors.white.withValues(alpha: 0.06)),
        ),
        child: Column(
          children: [
            Icon(icon, color: AppTheme.warmGold, size: 20),
            const SizedBox(height: 6),
            Text(
              label,
              style: GoogleFonts.dmSans(
                color: AppTheme.muted,
                fontSize: 11,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
