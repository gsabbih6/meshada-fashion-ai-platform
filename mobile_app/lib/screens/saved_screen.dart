import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:shimmer/shimmer.dart';
import '../theme/app_theme.dart';

class SavedScreen extends StatefulWidget {
  const SavedScreen({super.key});

  @override
  State<SavedScreen> createState() => _SavedScreenState();
}

class _SavedScreenState extends State<SavedScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 20, 20, 0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Your Collection',
                    style: GoogleFonts.dmSans(
                      fontSize: 34,
                      fontWeight: FontWeight.w300,
                      letterSpacing: -1.2,
                      color: AppTheme.warmCream,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Saved looks & wishlisted items',
                    style: GoogleFonts.dmSans(
                      fontSize: 14,
                      color: AppTheme.muted,
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 24),

            // Tabs
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20),
              child: Container(
                decoration: BoxDecoration(
                  color: AppTheme.cardBg,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: TabBar(
                  controller: _tabController,
                  indicator: BoxDecoration(
                    color: AppTheme.warmCream,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  labelColor: AppTheme.scaffoldBg,
                  unselectedLabelColor: AppTheme.muted,
                  indicatorSize: TabBarIndicatorSize.tab,
                  dividerColor: Colors.transparent,
                  labelStyle: GoogleFonts.dmSans(
                    fontWeight: FontWeight.w600,
                    fontSize: 13,
                  ),
                  unselectedLabelStyle: GoogleFonts.dmSans(
                    fontWeight: FontWeight.w400,
                    fontSize: 13,
                  ),
                  padding: const EdgeInsets.all(4),
                  tabs: const [
                    Tab(text: 'Saved Looks'),
                    Tab(text: 'Wishlist'),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 24),

            // Tab content
            Expanded(
              child: TabBarView(
                controller: _tabController,
                children: [
                  _SavedLooksTab(),
                  _WishlistTab(),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SavedLooksTab extends StatelessWidget {
  final List<Map<String, String>> savedLooks = const [
    {
      'image':
          'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=400',
      'title': 'Summer Evening Look',
      'model': 'Aria',
      'items': '3 items',
    },
    {
      'image':
          'https://images.unsplash.com/photo-1539109136881-3be0616acf4b?w=400',
      'title': 'Street Style Edit',
      'model': 'Luna',
      'items': '5 items',
    },
    {
      'image':
          'https://images.unsplash.com/photo-1496747611176-843222e1e57c?w=400',
      'title': 'Minimal Chic',
      'model': 'Nova',
      'items': '2 items',
    },
  ];

  @override
  Widget build(BuildContext context) {
    if (savedLooks.isEmpty) {
      return _buildEmptyState(
        icon: Icons.bookmark_border_rounded,
        title: 'No saved looks yet',
        subtitle: 'Double-tap videos in your feed to save looks you love',
      );
    }

    return ListView.separated(
      padding: const EdgeInsets.symmetric(horizontal: 20),
      itemCount: savedLooks.length,
      separatorBuilder: (_, __) => const SizedBox(height: 16),
      itemBuilder: (context, index) {
        final look = savedLooks[index];
        return _SavedLookCard(look: look);
      },
    );
  }

  Widget _buildEmptyState({
    required IconData icon,
    required String title,
    required String subtitle,
  }) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 40),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                    color: AppTheme.warmGold.withValues(alpha: 0.2)),
              ),
              child: Icon(icon,
                  color: AppTheme.warmGold.withValues(alpha: 0.5), size: 36),
            ),
            const SizedBox(height: 20),
            Text(
              title,
              style: GoogleFonts.dmSans(
                color: AppTheme.warmCream,
                fontSize: 18,
                fontWeight: FontWeight.w500,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              subtitle,
              style: const TextStyle(
                color: AppTheme.muted,
                fontSize: 14,
                height: 1.4,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}

class _SavedLookCard extends StatelessWidget {
  final Map<String, String> look;

  const _SavedLookCard({required this.look});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 120,
      decoration: BoxDecoration(
        color: AppTheme.cardBg,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: Colors.white.withValues(alpha: 0.04)),
      ),
      child: Row(
        children: [
          // Thumbnail
          ClipRRect(
            borderRadius:
                const BorderRadius.horizontal(left: Radius.circular(14)),
            child: SizedBox(
              width: 100,
              child: CachedNetworkImage(
                imageUrl: look['image']!,
                fit: BoxFit.cover,
                height: double.infinity,
                placeholder: (_, __) => Shimmer.fromColors(
                  baseColor: AppTheme.cardBg,
                  highlightColor: const Color(0xFF222222),
                  child: Container(color: AppTheme.cardBg),
                ),
                errorWidget: (_, __, ___) => Container(
                  color: AppTheme.cardBg,
                  child: const Icon(Icons.broken_image, color: AppTheme.muted),
                ),
              ),
            ),
          ),

          // Info
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    look['title']!,
                    style: GoogleFonts.dmSans(
                      color: AppTheme.warmCream,
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'by ${look['model']}',
                    style: const TextStyle(
                        color: AppTheme.warmGold, fontSize: 12),
                  ),
                  const SizedBox(height: 10),
                  Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.06),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      look['items']!,
                      style: const TextStyle(
                          color: AppTheme.muted, fontSize: 11),
                    ),
                  ),
                ],
              ),
            ),
          ),

          // Action
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: Colors.white.withValues(alpha: 0.06),
              ),
              child: const Icon(Icons.arrow_forward_rounded,
                  color: AppTheme.warmCream, size: 16),
            ),
          ),
        ],
      ),
    );
  }
}

class _WishlistTab extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 40),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                    color: AppTheme.softRose.withValues(alpha: 0.2)),
              ),
              child: Icon(Icons.favorite_border_rounded,
                  color: AppTheme.softRose.withValues(alpha: 0.5), size: 36),
            ),
            const SizedBox(height: 20),
            Text(
              'Your wishlist is empty',
              style: GoogleFonts.dmSans(
                color: AppTheme.warmCream,
                fontSize: 18,
                fontWeight: FontWeight.w500,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            const Text(
              'Heart items from the feed or discover page to add them here',
              style: TextStyle(
                color: AppTheme.muted,
                fontSize: 14,
                height: 1.4,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}
